package ru.test.impl.dto;

import ru.test.impl.entity.Jms;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.StringJoiner;

@XmlRootElement
public class JmsDto {

    private String id;

    private String content;

    public JmsDto() {
    }

    public JmsDto(String id, String content) {
        this.id = id;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", JmsDto.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("content='" + content + "'")
                .toString();
    }

    public static JmsDto from(Jms jms) {
        return new JmsDto(jms.getId(), jms.getContent());
    }

    public static Jms to(JmsDto jmsDto, Jms jms) {
        jms.setId(jmsDto.getId());
        jms.setContent(jmsDto.getContent());
        return jms;
    }
}
