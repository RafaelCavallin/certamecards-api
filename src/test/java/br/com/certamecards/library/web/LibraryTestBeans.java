package br.com.certamecards.library.web;

import br.com.certamecards.subject.persistence.SubjectRepository;
import br.com.certamecards.support.MutableClock;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

record LibraryTestBeans(
        MockMvc mockMvc,
        ObjectMapper objectMapper,
        JdbcClient jdbcClient,
        JavaMailSender mailSender,
        MutableClock clock,
        SubjectRepository subjects) {}
