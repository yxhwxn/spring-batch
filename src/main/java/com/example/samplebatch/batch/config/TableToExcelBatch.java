package com.example.samplebatch.batch.config;

import com.example.samplebatch.batch.utils.ExcelRowWriter;
import com.example.samplebatch.entity.BeforeEntity;
import com.example.samplebatch.repository.BeforeRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.util.Map;

@Configuration
public class TableToExcelBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final BeforeRepository beforeRepository;

    public TableToExcelBatch(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager, BeforeRepository beforeRepository) {
        this.jobRepository = jobRepository;
        this.platformTransactionManager = platformTransactionManager;
        this.beforeRepository = beforeRepository;
    }

    @Bean
    public Job TTEJob() {

        System.out.println("Table to Excel job");

        return new JobBuilder("TTEJob", jobRepository)
                .start(TTEStep())
                .build();
    }

    @Bean
    public Step TTEStep() {

        System.out.println("Table to Excel step");

        return new StepBuilder("TTEStep", jobRepository)
                .<BeforeEntity, BeforeEntity>chunk(10, platformTransactionManager)
                .reader(TTEBeforeReader())
                .processor(middleProcessor())
                .writer(excelWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<BeforeEntity> TTEBeforeReader() {

        RepositoryItemReader<BeforeEntity> reader = new RepositoryItemReaderBuilder<BeforeEntity>()
                .name("beforeReader")
                .pageSize(10)
                .methodName("findAll")
                .repository(beforeRepository)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();

        // Excel to Table 배치 예제와 다른점: 전체 데이터 셋에서 어디까지 수행 했는지의 값을 저장하지 않음
        // 왜냐면! -> 파일로 만들어내야 하는데, 중간이 끊기는 것을 기록할 필요가 없음(어차피 하나의 파일이 미완성된 상태니까)
        reader.setSaveState(false);

        return reader;
    }

    @Bean
    public ItemProcessor<BeforeEntity, BeforeEntity> middleProcessor() {

        return item -> item;
    }

    @Bean
    public ItemStreamWriter<BeforeEntity> excelWriter() {

        try {
            return new ExcelRowWriter("/Users/yh/Desktop/woori-fisa/final_project/spring-batch/TableToExcel_sample.xlsx");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
