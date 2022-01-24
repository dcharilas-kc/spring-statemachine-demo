package demo.statemachine.converter;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.kryo.MessageHeadersSerializer;
import org.springframework.statemachine.kryo.StateMachineContextSerializer;
import org.springframework.statemachine.kryo.UUIDSerializer;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static java.util.Objects.isNull;


@RequiredArgsConstructor
@Converter(autoApply = true)
public class StateMachineContextConverter implements AttributeConverter<StateMachineContext<?, ?>, byte[]> {
  
  @SuppressWarnings("rawtypes")
  private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
    Kryo kryo = new Kryo();
    kryo.addDefaultSerializer(StateMachineContext.class, new StateMachineContextSerializer());
    kryo.addDefaultSerializer(MessageHeaders.class, new MessageHeadersSerializer());
    kryo.addDefaultSerializer(UUID.class, new UUIDSerializer());
    return kryo;
  });
  
  @Override
  public byte[] convertToDatabaseColumn(StateMachineContext<?, ?> context) {
    if (isNull(context)) {
      return null;
    }
    
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Output output = new Output(byteArrayOutputStream);
    
    KRYO_THREAD_LOCAL.get().writeObject(output, context);
    
    output.flush();
    output.close();
    
    return byteArrayOutputStream.toByteArray();
  }
  
  @Override
  public StateMachineContext<?, ?> convertToEntityAttribute(byte[] dbData) {
    if (isNull(dbData) || dbData.length == 0) {
      return null;
    }
    
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(dbData);
    Input input = new Input(byteArrayInputStream);
    return KRYO_THREAD_LOCAL.get().readObject(input, StateMachineContext.class);
  }
  
}
