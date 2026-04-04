import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RabbitTest {


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void testSend() {
        rabbitTemplate.convertAndSend("pay.topic", "pay.success", "hello");
    }
}