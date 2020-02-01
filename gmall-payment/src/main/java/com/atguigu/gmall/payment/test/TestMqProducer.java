package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class TestMqProducer {
    public static void main(String[] args) {
        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://192.168.1.107:61616");
        try {
            Connection connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED); //开启事务的消息
            Queue testqueue = session.createQueue("drink");

            MessageProducer producer = session.createProducer(testqueue);
            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("我渴了，谁能帮我倒杯水！");
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(textMessage);
            session.commit();//提交事务
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
