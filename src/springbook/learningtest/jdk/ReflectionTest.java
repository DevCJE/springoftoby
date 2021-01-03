package springbook.learningtest.jdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ReflectionTest {

    @Test
    public void invokeMethod() throws Exception {
        String name = "Spring";

        // length()
        assertThat(name.length(), is(6));

        // 리플렉션 API 중에서 메소드에 대한 정의를 담은 Method라는 인터페이스를 이용해 메소드를 호출하는 방법.
        // 스트링이 가진 메소드 중에서 "length"라는 이름을 갖고 있고, 파라미터는 없는 메소드의 정보를 가져오는 것이다.
        Method lengthMethod = String.class.getMethod("length");
        assertThat((Integer)lengthMethod.invoke(name), is(6));

        // charAt()
        assertThat(name.charAt(0), is('S'));

        // 또한 Method 인터페이스에서 정의한 부분을 실행시킬 수도 있는데 invoke() 메소드를 이용해서 실행한다.
        // invoke() 메소드는 메소드를 실행시킬 대상 오브젝트(obj)와 파라미터 목록(args)을 받아서 메소드를 호출한 뒤
        // 그 결과를 Object 타입으로 돌려준다.
        Method charAtMethod = String.class.getMethod("charAt", int.class);
        assertThat((Character)charAtMethod.invoke(name,0), is('S'));

    }

    @Test
    public void simpleProxy(){

        Hello hello = new HelloTarget();
        assertThat(hello.sayHello("Toby"), is("Hello Toby"));
        assertThat(hello.sayHi("Toby"), is("Hi Toby"));
        assertThat(hello.sayThankYou("Toby"), is("Thank You Toby"));

    }

    @Test
    public void upperCase(){
        //프록시를 통해 타깃 오브젝트에 접근하도록 구성하고 프록시에서는 모든 문자열을 대문자로 바꿔주는 부가기능을 담당한다.
        Hello proxiedHello = new HelloUppercase(new HelloTarget());
        assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
        assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
        assertThat(proxiedHello.sayThankYou("Toby"), is("THANK YOU TOBY"));
    }

    @Test
    public void invocationTest(){
        // 위에 upperCase() 메소드에서 프록시를 만드는 방법을 다이나믹 프록시를 이용하여 만든법 외에는 차이가 없다.

        /*
        첫 번째 파라미터는 클래스 로더를 제공해야 한다. 다이내믹 프록시가 정의되는 클래스 로더를 지정하는 것이다.
        두 번째 파라미터는 다이내믹 프록시가 구현해야 할 인터페이스다.
        다이내믹 프록시는 한 번에 하나 이상의 인터페이스를 구현할 수도 있다. 따라서 인터페이스의 배열을 사용한다.
        마지막 파라미터로는 부가기능과 위임 관련 코드를 담고 있는 InvocationHandler 구현 오브젝트를 제공해야 한다.
        Hello 타입의 타깁 오브젝트를 생성자로 받고, 모든 메소드 호출의 리턴 값을 대문자로 바꿔주는 UppercaseHandler 오브젝트를 전달했다.
         */
        Hello proxiedHello = (Hello) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] { Hello.class },
                new UppercaseHandler(new HelloTarget())
        );
        assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
        assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
        assertThat(proxiedHello.sayThankYou("Toby"), is("THANK YOU TOBY"));
    }

}
