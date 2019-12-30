package cl.tenpo.customerauthentication.externalservice.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDto {

    private String from;
    private String to;
    private String bcc[];
    private String subject;
    private String template;

    @JsonProperty("referenceId")
    private String referenceId;
    private Map<String, String> params;

}
