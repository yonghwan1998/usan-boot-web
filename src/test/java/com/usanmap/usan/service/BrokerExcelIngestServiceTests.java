package com.usanmap.usan.service;

import com.usanmap.usan.batch.job.broker.support.BatchExcelProps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BrokerExcelIngestServiceTests {
    @TempDir Path inbox;

    @Test
    void runsOneJobForEachExcelInOrder() throws Exception {
        Files.createFile(inbox.resolve("b.xlsx"));
        Files.createFile(inbox.resolve("a.xlsx"));
        Files.createFile(inbox.resolve("ignored.txt"));
        Files.createDirectory(inbox.resolve("directory.xlsx"));
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        BatchExcelProps props = new BatchExcelProps();
        props.setInbox(inbox.toString());
        var inputs = new ArrayList<String>();
        when(launcher.run(eq(job), any())).thenAnswer(call -> {
            JobParameters params = call.getArgument(1);
            inputs.add(Path.of(params.getString("inputFile")).getFileName().toString());
            JobExecution execution = new JobExecution((long) inputs.size(), params);
            execution.setStatus(BatchStatus.COMPLETED);
            return execution;
        });
        new BrokerExcelIngestService(launcher, job, props).run();
        assertThat(inputs).containsExactly("a.xlsx", "b.xlsx");
    }

    @Test
    void stopsWhenFailedFileRemainsInInbox() throws Exception {
        Files.createFile(inbox.resolve("a.xlsx"));
        Files.createFile(inbox.resolve("b.xlsx"));
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        BatchExcelProps props = new BatchExcelProps();
        props.setInbox(inbox.toString());
        JobExecution failed = new JobExecution(1L);
        failed.setStatus(BatchStatus.FAILED);
        when(launcher.run(eq(job), any())).thenReturn(failed);
        assertThatThrownBy(() -> new BrokerExcelIngestService(launcher, job, props).run())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("a.xlsx");
        verify(launcher, times(1)).run(eq(job), any());
    }
    @Test
    void continuesAfterFailedFileIsMovedToError() throws Exception {
        Path first = Files.createFile(inbox.resolve("a.xlsx"));
        Path second = Files.createFile(inbox.resolve("b.xlsx"));
        Path error = Files.createDirectory(inbox.resolve("error"));
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        BatchExcelProps props = new BatchExcelProps();
        props.setInbox(inbox.toString());
        props.setError(error.toString());
        var inputs = new ArrayList<String>();
        when(launcher.run(eq(job), any())).thenAnswer(call -> {
            JobParameters params = call.getArgument(1);
            Path file = Path.of(params.getString("inputFile"));
            inputs.add(file.getFileName().toString());
            JobExecution execution = new JobExecution((long) inputs.size(), params);
            execution.setStatus(file.equals(first) ? BatchStatus.FAILED : BatchStatus.COMPLETED);
            if (execution.getStatus() == BatchStatus.FAILED) {
                // 실제 afterJob과 같은 실패 파일 이동 처리를 수행한다.
                execution.addFailureException(new org.apache.poi.util.RecordFormatException("array exceeds limit"));
                new com.usanmap.usan.batch.job.broker.support.FailedExcelHandler(props).handle(execution);
            }
            return execution;
        });
        new BrokerExcelIngestService(launcher, job, props).run();
        assertThat(inputs).containsExactly("a.xlsx", "b.xlsx");
        assertThat(first).doesNotExist();
        assertThat(error.resolve("a.xlsx")).exists();
        assertThat(second).exists();
    }

}
