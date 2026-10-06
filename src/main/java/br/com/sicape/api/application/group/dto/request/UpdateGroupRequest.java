package br.com.sicape.api.application.group.dto.request;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.sicape.api.domain.enums.GroupStatus;

public class UpdateGroupRequest {
    private String name;
    private boolean nameProvided;

    private String subject;
    private boolean subjectProvided;

    private List<String> presenters;
    private boolean presentersProvided;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate predictedEndDate;
    private boolean predictedEndDateProvided;

    private GroupStatus status;
    private boolean statusProvided;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private boolean startDateProvided;
    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.nameProvided = true;
    }

    public boolean isNameProvided() {
        return nameProvided;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
        this.subjectProvided = true;
    }

    public boolean isSubjectProvided() {
        return subjectProvided;
    }

    public List<String> getPresenters() {
        return presenters;
    }

    public void setPresenters(List<String> presenters) {
        this.presenters = presenters;
        this.presentersProvided = true;
    }

    public boolean isPresentersProvided() {
        return presentersProvided;
    }

    public LocalDate getPredictedEndDate() {
        return predictedEndDate;
    }

    public void setPredictedEndDate(LocalDate predictedEndDate) {
        this.predictedEndDate = predictedEndDate;
        this.predictedEndDateProvided = true;
    }

    public boolean isPredictedEndDateProvided() {
        return predictedEndDateProvided;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public void setStatus(GroupStatus status) {
        this.status = status;
        this.statusProvided = true;
    }

    public boolean isStatusProvided() {
        return statusProvided;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        this.startDateProvided = true;
    }

    public boolean isStartDateProvided() {
        return startDateProvided;
    }

    public boolean hasChanges() {
        return nameProvided || subjectProvided || presentersProvided || predictedEndDateProvided
            || statusProvided || startDateProvided;
    }

    public Set<String> getUnsupportedFields() {
        return Set.copyOf(unsupportedFields);
    }

    @JsonAnySetter
    public void addUnsupportedField(String name, Object value) {
        unsupportedFields.add(name);
    }
}