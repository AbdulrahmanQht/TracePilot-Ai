package com.tracepilot.api.Integration;

import org.junit.jupiter.api.AfterEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.tracepilot.api.TestcontainersConfiguration;
import com.tracepilot.api.Repositories.AgentReportRepository;
import com.tracepilot.api.Repositories.ReliabilityHistoryRepository;
import com.tracepilot.api.Repositories.TraceAuditRepository;
import com.tracepilot.api.Repositories.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TraceAuditRepository auditRepository;

    @Autowired
    protected AgentReportRepository agentReportRepository;

    @Autowired
    protected ReliabilityHistoryRepository reliabilityHistoryRepository;

    @MockitoBean
    protected JavaMailSender javaMailSender;

    @AfterEach
    void drainTestQueues() {
        while (rabbitTemplate.receive("audit.jobs", 50) != null) {
        }
        while (rabbitTemplate.receive("audit.results", 50) != null) {
        }
    }
}
