package com.example.demo.config;

import com.example.demo.entity.Student;
import com.example.demo.repo.StudentRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private StudentRepository studentRepository;

    @Bean
    public Job importStudentsJob() {
        return jobBuilderFactory.get("importStudentsJob")
                .incrementer(new RunIdIncrementer())
                .start(importStudentsStep())
                .build();
    }

    @Bean
    public Step importStudentsStep() {
        return stepBuilderFactory.get("importStudentsStep")
                .<Student, Student>chunk(10)  // Process 10 items per chunk
                .reader(studentReader())
                .writer(studentWriter())
                .build();
    }

    @Bean
    public FlatFileItemReader<Student> studentReader() {
        FlatFileItemReader<Student> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("students.csv"));  // Path to your CSV file
        reader.setLinesToSkip(1);  // Skip the header line in the CSV file

        // Map CSV lines to Student objects (skip id in CSV)
        reader.setLineMapper(new DefaultLineMapper<>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames("name", "email", "age");  // Ensure these match the CSV column names
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType(Student.class);  // Map to Student entity
            }});
        }});

        return reader;
    }

    @Bean
    public ItemWriter<Student> studentWriter() {
        return students -> {
            students.forEach(student -> {
                System.out.println("Saving student: " + student);  // Log student data
            });
            studentRepository.saveAll(students);  // Save the list of students to the database
        };
    }

}
