import com.smart.watering.system.be.config.mqtt.MqttProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(MqttProps.class)
@SpringBootApplication
public class Application {
    static void main(String[] args){
        SpringApplication.run(Application.class, args);
    }
}
