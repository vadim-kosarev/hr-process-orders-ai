package hr.orders.domain.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "commandType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateOrderCommand.class, name = "CREATE_ORDER")
})
public abstract class OrderCommand implements Serializable {

    protected UUID commandId;
    protected LocalDateTime issuedAt;

    public abstract String getCommandType();

    protected void initCommand() {
        this.commandId = UUID.randomUUID();
        this.issuedAt = LocalDateTime.now();
    }
}

