package cl.tenpo.customerauthentication.externalservice.notification.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Jose Hurtado on 5/15/2019.
 */
@Getter
@Setter
@Builder
public class PurchaseMessageDto {
    private String title;
    private String currencyType;
    private String registerMessage;
}
