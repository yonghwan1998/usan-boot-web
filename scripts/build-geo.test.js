const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { normalizeSigunguFeatures } = require('./build-geo');

function feature(code, name) {
    return { type: 'Feature', properties: { BJCD: code, NAME: name },
        geometry: { type: 'Polygon', coordinates: [[[0, 0], [1, 0], [0, 1], [0, 0]]] } };
}

test('일반구가 있는 시 경계를 제거하고 구 이름에 시 이름을 붙인다', () => {
    const city = feature('4146000000', '용인시');
    const districts = [feature('4146100000', '처인구'), feature('4146300000', '기흥구'),
        feature('4146500000', '용인시 수지구')];
    const source = [city, ...districts];
    const snapshot = JSON.stringify(source);
    const actual = normalizeSigunguFeatures(source);
    assert.deepEqual(actual.map(f => f.properties.NAME), ['용인시 처인구', '용인시 기흥구', '용인시 수지구']);
    actual.forEach((f, i) => {
        assert.equal(f.properties.BJCD, districts[i].properties.BJCD);
        assert.strictEqual(f.geometry, districts[i].geometry);
    });
    assert.equal(JSON.stringify(source), snapshot);
});

test('자치구, 일반 시·군과 코드 앞 네 자리가 같은 군은 유지한다', () => {
    const source = [feature('1168000000', '강남구'), feature('4121000000', '광명시'),
        feature('4374000000', '영동군'), feature('4374500000', '증평군')];
    assert.deepEqual(normalizeSigunguFeatures(source), source);
});

test('구가 없는 시는 유지하고 원본 순서와 관계없이 정규화한다', () => {
    const district = feature('4119200000', '원미구');
    assert.deepEqual(normalizeSigunguFeatures([district, feature('4119000000', '부천시')])
        .map(f => f.properties.NAME), ['부천시 원미구']);
    const city = feature('4119000000', '부천시');
    assert.deepEqual(normalizeSigunguFeatures([city]), [city]);
});

const districtCities = {
    '41110': ['수원시', 4], '41130': ['성남시', 3], '41170': ['안양시', 2],
    '41190': ['부천시', 3], '41270': ['안산시', 2], '41280': ['고양시', 3],
    '41460': ['용인시', 3], '43110': ['청주시', 4], '44130': ['천안시', 2],
    '47110': ['포항시', 2], '48120': ['창원시', 5], '52110': ['전주시', 2]
};
const geoDir = path.join(__dirname, '../src/main/resources/static/geo');
const allFeatures = fs.readdirSync(path.join(geoDir, 'sigungu')).flatMap(file =>
    JSON.parse(fs.readFileSync(path.join(geoDir, 'sigungu', file), 'utf8')).features);

test('실제 전국 GeoJSON은 일반구 35개를 시 이름과 함께 제공하고 상위 시 경계는 제외한다', () => {
    for (const [code, [cityName, count]] of Object.entries(districtCities)) {
        const features = allFeatures.filter(f => f.properties.shortAdmCd.startsWith(code.slice(0, 4)));
        assert.equal(features.length, count, cityName);
        assert.ok(features.every(f => f.properties.shortAdmCd !== code), cityName);
        assert.ok(features.every(f => f.properties.name.startsWith(cityName + ' ')), cityName);
        for (const f of features) {
            assert.equal(f.properties.level, 'SIGUNGU');
            assert.equal(f.properties.parentAdmCd, code.slice(0, 2));
            assert.ok(fs.existsSync(path.join(geoDir, 'emd', f.properties.shortAdmCd + '.geo.json')));
        }
    }
    assert.equal(allFeatures.find(f => f.properties.shortAdmCd === '43745').properties.name, '증평군');
    assert.equal(allFeatures.find(f => f.properties.shortAdmCd === '11680').properties.name, '강남구');
});

function insideRing(point, ring) {
    let inside = false;
    for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
        const [xi, yi] = ring[i];
        const [xj, yj] = ring[j];
        if ((yi > point[1]) !== (yj > point[1]) &&
            point[0] < (xj - xi) * (point[1] - yi) / (yj - yi) + xi) inside = !inside;
    }
    return inside;
}

function contains(geometry, point) {
    const polygons = geometry.type === 'Polygon' ? [geometry.coordinates] : geometry.coordinates;
    return polygons.some(rings => insideRing(point, rings[0]) &&
        !rings.slice(1).some(ring => insideRing(point, ring)));
}

test('용인시 3개 구의 내부 좌표는 각각 하나의 구 경계에만 속한다', () => {
    const points = [
        [127.2015, 37.2342, '용인시 처인구'],
        [127.1147, 37.2804, '용인시 기흥구'],
        [127.0981, 37.3221, '용인시 수지구']
    ];
    for (const [lng, lat, name] of points) {
        const matches = allFeatures.filter(f => contains(f.geometry, [lng, lat]));
        assert.deepEqual(matches.map(f => f.properties.name), [name]);
    }
});
