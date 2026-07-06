package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "app must not be blank")
    private String app;

    @NotBlank(message = "uri must not be blank")
    private String uri;

    @NotBlank(message = "ip must not be blank")
    private String ip;

    @NotBlank(message = "timestamp must not be blank")
    @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}",
            message = "timestamp must match yyyy-MM-dd HH:mm:ss"
    )
    private String timestamp;
}
