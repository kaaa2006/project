package org.team.mealkitshop.dto.address;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AddressUpdateDTO {

    @NotNull
    private Long id;

    @Size(max = 50)
    private String alias;

    @Size(max = 10)
    private String zipCode;

    @Size(max = 100)
    private String addr1;

    @Size(max = 100)
    private String addr2;

    private Boolean isDefault;
}
