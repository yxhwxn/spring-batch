package com.example.samplebatch.controller;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
public class MainController {

    private final JobLauncher jobLauncher;  // job을 시작할 수 있도록
    private final JobRegistry jobRegistry;  // TableToTableBatch1 처럼 배치 job을 가져올 수 있도록

    public MainController(JobLauncher jobLauncher, JobRegistry jobRegistry) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
    }

    /**
     * API 호출을 통해 value 값을 jobParameter로 할당해줘서 TableToTableBatchJob1 이라는 배치 프로세스를 트리거 할 수 있음
     *
     * @param value
     * @return
     * @throws Exception
     */
    @GetMapping("/table-to-table")
    public String firstApi(@RequestParam("value") String value) throws Exception {

        // job이 특정 이름과 특정 번호로 실행될 수 있도록 파라미터 지정(날짜와, 배치 job 식별 값)
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", value)
                .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("TableToTableBatch1Job"), jobParameters);


        return "table-to-table job is successes!";
    }
}