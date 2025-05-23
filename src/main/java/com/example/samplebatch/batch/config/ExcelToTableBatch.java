package com.example.samplebatch.batch.config;

import com.example.samplebatch.batch.utils.ExcelRowReader;
import com.example.samplebatch.entity.AfterEntity;
import com.example.samplebatch.repository.AfterRepository;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

@Configuration
public class ExcelToTableBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final AfterRepository afterRepository;

    public ExcelToTableBatch(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager, AfterRepository afterRepository) {
        this.jobRepository = jobRepository;
        this.platformTransactionManager = platformTransactionManager;
        this.afterRepository = afterRepository;
    }

    @Bean
    public Job E2TJob() {

        System.out.println("Excel to Table(DB) job");

        return new JobBuilder("E2TJob", jobRepository)
                .start(E2TStep())
                .build();
    }

    @Bean
    public Step E2TStep() {

        return new StepBuilder("E2TStep", jobRepository)
                .<Row, AfterEntity>chunk(10, platformTransactionManager)
                .reader(excelReader())
                .processor(fourthProcessor())
                .writer(fourthAfterWriter())
                .build();
    }

    @Bean
    public ItemStreamReader<Row> excelReader() {

        try {
            return new ExcelRowReader("/Users/yh/Desktop/woori-fisa/final_project/spring-batch/batch_sample.xlsx");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public ItemProcessor<Row, AfterEntity> fourthProcessor() {

        return new ItemProcessor<Row, AfterEntity>() {

            @Override
            public AfterEntity process(Row item) {

                AfterEntity afterEntity = new AfterEntity();
                afterEntity.setUsername(item.getCell(0).getStringCellValue());  // (참고) 준비된 샘플 엑셀 시트가 a열에 더미 데이터 이름이 있는 상태

                return afterEntity;
            }
        };
    }

    @Bean
    public RepositoryItemWriter<AfterEntity> fourthAfterWriter() {

        return new RepositoryItemWriterBuilder<AfterEntity>()
                .repository(afterRepository)
                .methodName("save")
                .build();
    }
}
