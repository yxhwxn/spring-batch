package com.example.samplebatch.batch;

import com.example.samplebatch.entity.AfterEntity;
import com.example.samplebatch.entity.BeforeEntity;
import com.example.samplebatch.repository.AfterRepository;
import com.example.samplebatch.repository.BeforeRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class TableToTableSimpleBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    private final BeforeRepository beforeRepository;
    private final AfterRepository afterRepository;

    public TableToTableSimpleBatch(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager, BeforeRepository beforeRepository, AfterRepository afterRepository) {

        this.jobRepository = jobRepository;
        this.platformTransactionManager = platformTransactionManager;
        this.beforeRepository = beforeRepository;
        this.afterRepository = afterRepository;
    }

    // Job 정의는 메서드 형태로
    @Bean
    public Job TableToTableBatch1Job() {

        System.out.println("Table to Table job");

        return new JobBuilder("T2T_Job", jobRepository)    // job 이름, 트래킹을 위한 repository(메타 데이터 테이블에 기록하기 위한 용도)
                .start(TableToTableStep())  // start(스텝이 들어갈 자리, step의 메서드명을 넣어주면 됨)
//                여러가지 step을 next로 정의할 수 있음
//                .next()
//                .next()
                .build();
    }

    /**
     * 청크 : chunk
     * 이때 읽기 → 처리 → 쓰기 작업은 청크 단위로 진행되는데, 대량의 데이터를 얼만큼 끊어서 처리할지에 대한 값으로 적당한 값을 선정해야 합니다.
     * <p>
     * (너무 작으면 I/O 처리가 많아지고 오버헤드 발생, 너무 크면 적재 및 자원 사용에 대한 비용과 실패시 부담이 커짐)
     */
    @Bean
    public Step TableToTableStep() {

        System.out.println("T2T_step");

        return new StepBuilder("T2T_Step", jobRepository)
                .<BeforeEntity, AfterEntity>chunk(10, platformTransactionManager)
                .reader(beforeReader())
                .processor(middleProcessor())
                .writer(afterWriter())
                .build();
    }

    // ************************************** Read -> Process -> Write 작성 **************************************

    /**
     * 아주 다양한 Reader 인터페이스와 구현체들이 존재하지만, 우리는 JPA를 통한 쿼리를 수행하기 때문에 RepositoryItemReader를 사용합니다.
     * <p>
     * 이때 청크 단위까지만 읽기 때문에 findAll을 하더라도 chunk 개수 만큼 사용하게 됩니다.
     * <p>
     * 따라서 자원 낭비를 방지하기 위해 Sort를 진행하고 pageSize() 단위를 설정해 findAll이 아닌 페이지 만큼 읽어올 수 있도록 설정합니다.
     */
    @Bean
    public RepositoryItemReader<BeforeEntity> beforeReader() {

        return new RepositoryItemReaderBuilder<BeforeEntity>()
                .name("beforeReader")
                .pageSize(10)
                .methodName("findAll")
                .repository(beforeRepository)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }

    /**
     * Process : 읽어온 데이터를 처리하는 Process (큰 작업을 수행하지 않을 경우 생략 가능, 지금과 같이 단순 이동은 사실 필요 없음)
     */
    @Bean
    public ItemProcessor<BeforeEntity, AfterEntity> middleProcessor() {

        return new ItemProcessor<BeforeEntity, AfterEntity>() {

            @Override
            public AfterEntity process(BeforeEntity item) throws Exception {

                AfterEntity afterEntity = new AfterEntity();
                afterEntity.setUsername(item.getUsername());

                return afterEntity;
            }
        };
    }

    /**
     * Writer: Process에게 return 받은 객체를 읽어와서 실제 DB(Date_DB에 있는 AfterEntity 테이블)에 쿼리를 날리는 게 목표
     * Writer 또한 다양한 인터페이스 및 구현체가 존재하지만 JPA를 통한 쿼리를 날리는게 목표로 RepositoryItemWriter를 사용합니다.
     */
    @Bean
    public RepositoryItemWriter<AfterEntity> afterWriter() {

        return new RepositoryItemWriterBuilder<AfterEntity>()
                .repository(afterRepository)
                .methodName("save")
                .build();
    }
}
