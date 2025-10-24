package org.team.mealkitshop.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AddressCreateDTO {

    @Size(max = 50)
    private String alias;

    @NotBlank
    @Size(max = 10)
    private String zipCode;

    @NotBlank
    @Size(max = 100)
    private String addr1;

    @Size(max = 100)
    private String addr2;

    private Boolean isDefault = false;
}
