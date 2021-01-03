package springbook.learningtest.factoryBean;

import org.springframework.beans.factory.FactoryBean;

public class MessageFactoryBean implements FactoryBean<Message> {

    // 오브젝트를 생성할 때 필요한 정보를 팩토리 빈의 프로퍼티로 설정해서 대신 DI 받을 수 있게 한다.
    // 주입된 정보는 오브젝트 생성 중에 사용된다.
    String text;

    public void setText(String text) {
        this.text = text;
    }

    // 실제 빈으로 사용될 오브젝트를 직접 생성한다. 코드를 이용하기 때문에 복잡한 방식의 오브젝트 생성과 초기화 작업도 가능하다.
    @Override
    public Message getObject() throws Exception {
        return Message.newMessage(this.text);
    }

    @Override
    public Class<?> getObjectType() {
        return Message.class;
    }

    // getObject()가 돌려주는 오브젝트가 싱글톤인지 알려준다. 이 팩토리 빈은 매번 요청할 때마다 새로운 오브젝트를 만들므로
    // false 설정한다. 이것은 팩토리 빈의 동작방식에 관한 설정이고 만들어진 빈 오브젝트는 싱글톤으로 스프링이 관리해줄 수 있다.
    @Override
    public boolean isSingleton() {
        return false;
    }
}
