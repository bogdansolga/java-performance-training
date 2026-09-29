package net.safedata.performance.training.lab.nplus1;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lab_store")
public class StoreEntity {

    @Id
    private Long id;

    private String name;

    @OneToMany(mappedBy = "store", cascade = CascadeType.PERSIST)
    private List<SectionEntity> sections = new ArrayList<>();

    protected StoreEntity() {
    }

    public StoreEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<SectionEntity> getSections() {
        return sections;
    }
}
