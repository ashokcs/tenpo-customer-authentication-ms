package cl.tenpo.customerauthentication.externalservice.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
public class TwoFactorPushRequest {
    @JsonProperty("userId")
    private UUID userId;

    @JsonProperty("messageType")
    private MessageType messageType;

    @JsonProperty("pusherChannel")
    private String pusherChannel;

    @JsonProperty("pusherEvent")
    private NotificationEventType pusherEvent;

    @JsonProperty("userIdSender")
    private UUID userIdSender;

    @JsonProperty("linkId")
    private UUID linkId;

    @JsonProperty("verificationCode")
    private String verificationCode;

    @JsonProperty("purchaseMessage")
    private PurchaseMessageDto purchaseMessage;
}
