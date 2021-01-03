package springbook.customtest;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import springbook.learningtest.jdk.Hello;
import springbook.learningtest.jdk.HelloTarget;

import java.lang.reflect.Proxy;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-applicationContext.xml")
public class ProxyTest {

    @Test
    public void proxyFactoryBeanTest(){
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget());
        pfBean.addAdvice(new ProxyFactoryBeanTest.UppercaseAdvice2());

        Hello proxiedHello = (Hello)pfBean.getObject();
        assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
        assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
        assertThat(proxiedHello.sayThankYou("Toby"), is("THANK YOU TOBY"));
    }

    @Test
    public void pointcutAdvisor(){
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget());

        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.setMappedName("sayH*");

        pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut,new ProxyFactoryBeanTest.UppercaseAdvice2()));

        Hello proxiedHello = (Hello)pfBean.getObject();
        assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
        assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
        assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));
    }

    @Test
    public void classNamePointcutAdvisor(){
        NameMatchMethodPointcut classMatchPointcut = new NameMatchMethodPointcut();
        classMatchPointcut.setClassFilter(new ClassFilter() {
            @Override
            public boolean matches(Class<?> aClass) {
                return aClass.getSimpleName().startsWith("HelloT");
            }
        });
        classMatchPointcut.setMappedName("sayH*");
        checkAdviced(new HelloTarget(), classMatchPointcut, true);
        class HelloWorld extends HelloTarget {}
        checkAdviced(new HelloWorld(), classMatchPointcut, false);
        class HelloToby extends HelloTarget {}
        checkAdviced(new HelloToby(), classMatchPointcut, true);
    }

    private void checkAdviced(Hello target, Pointcut pointcut, boolean adviced) {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(target);
        pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new ProxyFactoryBeanTest.UppercaseAdvice2()));

        Hello proxiedHello = (Hello) pfBean.getObject();

        if (adviced) {
            // 메소드 선정 방식을 통해 어드바이스 적용
            assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
            assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
            assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));
        }
        else {
            // 어드바이스 적용 대상 후보에서 아예 탈락
            assertThat(proxiedHello.sayHello("Toby"), is("Hello Toby"));
            assertThat(proxiedHello.sayHi("Toby"), is("Hi Toby"));
            assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));
        }
    }

    // UppercaseAdvice는 스태틱 클래스이므로 이미 있는 것으로 대체하고 해당 부분에 대한 작성은 생략한다.
//    static class UppercaseAdvice implements MethodInterceptor {
//        @Override
//        public Object invoke(MethodInvocation methodInvocation) throws Throwable {
//            String ret = (String)methodInvocation.proceed();
//            return ret.toUpperCase();
//        }
//    }

}
