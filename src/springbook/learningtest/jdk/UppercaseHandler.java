package springbook.learningtest.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class UppercaseHandler implements InvocationHandler {

    Object target;
    // 다이내믹 프록시로부터 전달받은 요청을 다시 타깃 오브젝트에 위임해야 하기 때문에
    // 타깃 오브젝트를 주입받아 둔다.
    public UppercaseHandler(Hello target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 호출한 메소드의 리턴 타입이 String인 경우만 대문자 변경 기능을 적용하도록 수정.
        Object ret = method.invoke(target, args);
        if(ret instanceof String && method.getName().startsWith("say")) { // String으로 캐스팅이 가능하면 대문자열로 모두 치환한다.
            return ((String) ret).toUpperCase();
        } else { // 그렇지 않으면 Object 그대로 돌려준다.
            return ret;
        }

        /*
        Method charAtMethod = String.class.getMethod("charAt", int.class);
        assertThat((Character)charAtMethod.invoke(name,0), is('S'));

        위의 부분과 아래 부분에서의 차이는 String이라는 차이일뿐 위에서 타겟 오브젝트는 String 타입이라는 것뿐이니
        해당 부분에 대해서 오해하거나 착각하지 말자.

        String ret = (String)method.invoke(target, args); // 타깃으로 위임, 인터페이스의 메소드 호출에 모두 적용된다.
        return ret.toUpperCase(); // 부가기능 제공
        */
    }
}
