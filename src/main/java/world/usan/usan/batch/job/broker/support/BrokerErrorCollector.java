package world.usan.usan.batch.job.broker.support;

import org.springframework.stereotype.Component;
import world.usan.usan.batch.job.broker.reader.BrokerErrorRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class BrokerErrorCollector {

    private final List<BrokerErrorRow> list = Collections.synchronizedList(new ArrayList<>());

    public void add(BrokerErrorRow row) {
        list.add(row);
    }

    public List<BrokerErrorRow> snapshot() {
        return new ArrayList<>(list);
    }

    public void clear() {
        list.clear();
    }
}
