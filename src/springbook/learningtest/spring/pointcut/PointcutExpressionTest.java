package springbook.learningtest.spring.pointcut;

import org.junit.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.ProxyFactoryBean;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PointcutExpressionTest {

    @Test
    public void methodSignaturePointcut() throws SecurityException, NoSuchMethodException {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public int springbook.learningtest.spring.pointcut.Target.minus(int,int) " +
                "throws java.lang.RuntimeException)"); // Target 클래스 minus() 메소드 시그니처

        // Target.minus() 메소드 시그니처가 맞는지 확인하기 위해서는 클래스 필터와 메소드 매처를 각각 비교해 보면 된다.
        // 해당 Target 클래스의 minus() 메소드는 시그니처의 부합하기 때문에 true이다.
        assertThat(pointcut.getClassFilter().matches(Target.class) && pointcut.getMethodMatcher().matches(
                      Target.class.getMethod("minus", int.class, int.class), null), is(true));

        // Target.plus() 메소드 시그니처가 맞는지 확인하기 위해서는 클래스 필터와 메소드 매처를 각각 비교해 보면 된다.
        // 해당 Target 클래스의 plus() 메소드는 minus()메소드가 아니므로 false 이다.
        assertThat(pointcut.getClassFilter().matches(Target.class) && pointcut.getMethodMatcher().matches(
                     Target.class.getMethod("plus", int.class, int.class),null), is(false));

        // Bean.method() 메소드 시그니처가 맞는지 확인하기 위해서는 클래스 필터와 메소드 매처를 각각 비교해 보면 된다.
        // Target 클래스가 아닌 Bean 클래스이므로 false 이다.
        assertThat(pointcut.getClassFilter().matches(Bean.class) && pointcut.getMethodMatcher().matches(
                    Target.class.getMethod("method"), null), is(false));

    }

    @Test
    // 아래 targetClassPointcutMatches를 이용해 포인트컷 표현식에 대한 다양한 결과를 알아보는 테스트 메소드
    public void pointcut() throws Exception{
        targetClassPointcutMatches("execution(* *(..))", true, true, true, true, true, true);
        targetClassPointcutMatches("execution(* hello(..))", true, true, false, false, false, false);
        targetClassPointcutMatches("execution(* hello())", true, false, false, false, false, false);
        targetClassPointcutMatches("execution(* hello(String))", false, true, false, false, false, false);
        targetClassPointcutMatches("execution(* meth*(..))", false, false, false, false, true, true);
        targetClassPointcutMatches("execution(* *(int,int))", false, false, true, true, false, false);
        targetClassPointcutMatches("execution(* *())", true, false, false, false, true, true);
        targetClassPointcutMatches("execution(* springbook.learningtest.spring.pointcut.Target.*(..))", true, true, true, true, true, false);
    }

    // 표현식과 6개의 메소드에 대한 예상 결과를 주면 이를 검증하는 기능을 제공한다.
    public void targetClassPointcutMatches(String expression, boolean... expected) throws Exception{
        pointcutMatches(expression, expected[0], Target.class, "hello");
        pointcutMatches(expression, expected[1], Target.class, "hello", String.class);
        pointcutMatches(expression, expected[2], Target.class, "plus", int.class, int.class);
        pointcutMatches(expression, expected[3], Target.class, "minus", int.class, int.class);
        pointcutMatches(expression, expected[4], Target.class, "method");
        pointcutMatches(expression, expected[5], Bean.class, "method");
    }

    // 위의 methodSignaturePointcut()에 있는 메소드 표현식을 통한 포인트컷 생성과 검증 과정을 템플릿화 하였다.
    public void pointcutMatches(String expression, Boolean expected, Class<?> clazz, String methodName, Class<?>... args)
        throws Exception {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(expression);

        assertThat(pointcut.getClassFilter().matches(clazz) &&
                              pointcut.getMethodMatcher().matches(clazz.getMethod(methodName, args), null)
                                  , is(expected));
    }

}
