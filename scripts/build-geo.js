const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const ROOT = process.cwd();

const PATHS = {
    shpDir: path.join(ROOT, 'data', 'shp'),
    outputDir: path.join(ROOT, 'src', 'main', 'resources', 'static', 'geo'),
    tempDir: path.join(ROOT, '.tmp-geo')
};

const FILES = {
    sido: path.join(PATHS.shpDir, 'N3A_G0010000.shp'),
    sigungu: path.join(PATHS.shpDir, 'N3A_G0100000.shp'),
    emd: path.join(PATHS.shpDir, 'N3A_G0110000.shp')
};

const FIELDS = {
    code: 'BJCD',
    name: 'NAME'
};

function ensureDir(dirPath) {
    fs.mkdirSync(dirPath, { recursive: true });
}

function cleanDir(dirPath) {
    if (fs.existsSync(dirPath)) {
        fs.rmSync(dirPath, { recursive: true, force: true });
    }
    fs.mkdirSync(dirPath, { recursive: true });
}

function run(command) {
    console.log(`\n[RUN] ${command}\n`);
    execSync(command, { stdio: 'inherit' });
}

function validateInputFiles() {
    Object.entries(FILES).forEach(([key, filePath]) => {
        if (!fs.existsSync(filePath)) {
            throw new Error(`[${key}] 파일이 없습니다: ${filePath}`);
        }
    });
}

function readJson(filePath) {
    return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
}

function writeJson(filePath, obj) {
    fs.writeFileSync(filePath, JSON.stringify(obj));
}

function featureCollection(features) {
    return {
        type: 'FeatureCollection',
        features
    };
}

function toStringSafe(value) {
    return value == null ? '' : String(value);
}

function exportRawGeoJson(inputFile, outputFile, simplifyPercent) {
    run([
        `mapshaper "${inputFile}"`,
        `-proj wgs84`,
        `-simplify ${simplifyPercent} keep-shapes`,
        `-filter-fields ${FIELDS.code},${FIELDS.name}`,
        `-o format=geojson precision=0.000001 "${outputFile}"`
    ].join(' '));
}

function buildSido() {
    const tempFile = path.join(PATHS.tempDir, 'sido-raw.geo.json');
    const outputFile = path.join(PATHS.outputDir, 'sido.geo.json');

    exportRawGeoJson(FILES.sido, tempFile, '8%');

    const raw = readJson(tempFile);
    const features = raw.features.map((feature) => {
        const admCd = toStringSafe(feature.properties[FIELDS.code]);
        const name = toStringSafe(feature.properties[FIELDS.name]);

        return {
            ...feature,
            properties: {
                admCd,
                shortAdmCd: admCd.slice(0, 2),
                name,
                level: 'SIDO'
            }
        };
    });

    writeJson(outputFile, featureCollection(features));
    console.log('[DONE] sido.geo.json');
}

function buildSigungu() {
    const tempFile = path.join(PATHS.tempDir, 'sigungu-raw.geo.json');
    const splitDir = path.join(PATHS.outputDir, 'sigungu');

    exportRawGeoJson(FILES.sigungu, tempFile, '8%');

    const raw = readJson(tempFile);
    const grouped = new Map();

    for (const feature of normalizeSigunguFeatures(raw.features)) {
        const admCd = toStringSafe(feature.properties[FIELDS.code]);
        const name = toStringSafe(feature.properties[FIELDS.name]);
        const parentAdmCd = admCd.slice(0, 2);
        const shortAdmCd = admCd.slice(0, 5);

        const normalizedFeature = {
            ...feature,
            properties: {
                admCd,
                shortAdmCd,
                parentAdmCd,
                name,
                level: 'SIGUNGU'
            }
        };

        if (!grouped.has(parentAdmCd)) {
            grouped.set(parentAdmCd, []);
        }

        grouped.get(parentAdmCd).push(normalizedFeature);
    }

    for (const [parentAdmCd, features] of grouped.entries()) {
        writeJson(
            path.join(splitDir, `${parentAdmCd}.geo.json`),
            featureCollection(features)
        );
    }

    console.log(`[DONE] sigungu files: ${grouped.size}`);
}

// 일반구와 이를 포함하는 시 경계가 함께 제공되므로 시군구 레벨은 말단 경계만 사용한다.
function normalizeSigunguFeatures(features) {
    const cities = new Map(features
        .filter(feature => {
            const code = toStringSafe(feature.properties[FIELDS.code]);
            return code.slice(4, 5) === '0' && toStringSafe(feature.properties[FIELDS.name]).endsWith('시');
        })
        .map(feature => [toStringSafe(feature.properties[FIELDS.code]).slice(0, 5), feature]));
    const districtCities = new Set();
    const normalized = features.map(feature => {
        const code = toStringSafe(feature.properties[FIELDS.code]);
        const cityCode = code.slice(0, 4) + '0';
        const city = cities.get(cityCode);
        const name = toStringSafe(feature.properties[FIELDS.name]);
        if (code.slice(4, 5) === '0' || !name.endsWith('구') || !city) return feature;

        districtCities.add(cityCode);
        const cityName = city.properties[FIELDS.name];
        return {
            ...feature,
            properties: {
                ...feature.properties,
                [FIELDS.name]: name.startsWith(cityName + ' ') ? name : cityName + ' ' + name
            }
        };
    });
    return normalized.filter(feature =>
        !districtCities.has(toStringSafe(feature.properties[FIELDS.code]).slice(0, 5)));
}

function buildEmd() {
    const tempFile = path.join(PATHS.tempDir, 'emd-raw.geo.json');
    const splitDir = path.join(PATHS.outputDir, 'emd');

    exportRawGeoJson(FILES.emd, tempFile, '5%');

    const raw = readJson(tempFile);
    const grouped = new Map();

    for (const feature of raw.features) {
        const admCd = toStringSafe(feature.properties[FIELDS.code]);
        const name = toStringSafe(feature.properties[FIELDS.name]);
        const parentAdmCd = admCd.slice(0, 5);
        const shortAdmCd = admCd;

        const normalizedFeature = {
            ...feature,
            properties: {
                admCd,
                shortAdmCd,
                parentAdmCd,
                name,
                level: 'EMD'
            }
        };

        if (!grouped.has(parentAdmCd)) {
            grouped.set(parentAdmCd, []);
        }

        grouped.get(parentAdmCd).push(normalizedFeature);
    }

    for (const [parentAdmCd, features] of grouped.entries()) {
        writeJson(
            path.join(splitDir, `${parentAdmCd}.geo.json`),
            featureCollection(features)
        );
    }

    console.log(`[DONE] emd files: ${grouped.size}`);
}

function main() {
    validateInputFiles();

    cleanDir(PATHS.outputDir);
    cleanDir(PATHS.tempDir);

    ensureDir(path.join(PATHS.outputDir, 'sigungu'));
    ensureDir(path.join(PATHS.outputDir, 'emd'));

    buildSido();
    buildSigungu();
    buildEmd();

    fs.rmSync(PATHS.tempDir, { recursive: true, force: true });

    console.log('\n✅ 행정구역 GeoJSON 빌드 완료');
}

if (require.main === module) main();

module.exports = { normalizeSigunguFeatures };
