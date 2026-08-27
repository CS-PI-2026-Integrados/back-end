package br.com.sicape.api.application.convicted.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;

import br.com.sicape.api.domain.enums.EmploymentStatus;
import lombok.Getter;

@Getter
public class UpdateConvictedRequest {
    private String name;
    private String cpf;
    private LocalDate birthDate;
    private String phone;
    private UpdateAddressRequest address;
    private EmploymentStatus employmentStatus;
    private List<ConvictedProcessRequest> processes;

    private boolean nameProvided;
    private boolean cpfProvided;
    private boolean birthDateProvided;
    private boolean phoneProvided;
    private boolean addressProvided;
    private boolean employmentStatusProvided;
    private boolean processesProvided;

    @JsonSetter("name")
    public void setName(String value) { this.name = value; this.nameProvided = true; }

    @JsonSetter("cpf")
    public void setCpf(String value) { this.cpf = value; this.cpfProvided = true; }

    @JsonSetter("birth_date")
    public void setBirthDate(LocalDate value) { this.birthDate = value; this.birthDateProvided = true; }

    @JsonSetter("phone")
    public void setPhone(String value) { this.phone = value; this.phoneProvided = true; }

    @JsonSetter("address")
    public void setAddress(UpdateAddressRequest value) { this.address = value; this.addressProvided = true; }

    @JsonSetter("employment_status")
    public void setEmploymentStatus(EmploymentStatus value) {
        this.employmentStatus = value;
        this.employmentStatusProvided = true;
    }

    @JsonSetter("processes")
    public void setProcesses(List<ConvictedProcessRequest> value) {
        this.processes = value;
        this.processesProvided = true;
    }
}
