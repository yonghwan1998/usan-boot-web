package world.usan.usan.batch.job.broker.reader;

import world.usan.usan.batch.job.broker.support.BatchExcelProps;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * @date    2025-11-18
 * @author  yongss
 *
 * 처리 과정:
 *  - reader로 호출 될 때 특정 엑셀 파일 읽기
 *  - 읽은 데이터를 호출 된 메서드로 넘기기
 *  - MultiResourceItemReader는 resources로 정의된 파일들을 읽어와서, 파일 한 개씩 delegate로 정의된 reader가 read하도록 전달한다.
 */
public class BrokerMultiFileReader extends MultiResourceItemReader<BrokerRow> {

    private final ExcelRowItemReader delegate;
    private final BatchExcelProps props;

    public BrokerMultiFileReader(BatchExcelProps props) {
        this.props = props;
        this.delegate = new ExcelRowItemReader(props);
        setDelegate(delegate);
        setSaveState(true);
    }

    @Override
    public void open(ExecutionContext executionContext) {

        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("file:" + props.getInbox() + "/*.xlsx");
            setResources(resources);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve inbox resources", e);
        }

        super.open(executionContext);
    }
}
