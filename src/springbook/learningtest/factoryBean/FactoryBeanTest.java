package springbook.learningtest.factoryBean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration // 설정파일 이름을 지정하지 않으면 클래스 이름 + "-context.xml"이 디폴트로 사용된다.
public class FactoryBeanTest {

    //@Autowired는 같은 타입의 빈이 두 개 이상 있는 경우에는 타입만으로는 어떤 빈을 가져올 지 결정할 수 없다.
    // 이럴 때에는 변수의 이름과 같은 이름의 빈이 있는지 확인하고 변수의 이름과 같은 빈을 주입하며, 이마저도 없을 경우에는 예외를 호출
    @Autowired
    ApplicationContext context;

    // 해당 테스트를 통해서 팩토리빈에서 생성한 오브젝트가 실제 빈의 오브젝트로 대체됨을 확인할 수 있다.
    @Test
    public void getMessageFromFactoryBean(){
        Object message = context.getBean("message");
        assertThat(message, is(Message.class)); // 타입 확인
        assertThat(((Message)message).getText(), is("Factory Bean")); // 설정과 기능 확인
    }

    // 순수하게 팩토리 빈 자체를 가져오고 싶은 경우에는 아래와 같이 할 수 있음을 알 수 있다.
    @Test
    public void getFactoryBean(){
        Object factory = context.getBean("&message");
        assertThat(factory, is(MessageFactoryBean.class));
    }

}
