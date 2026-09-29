package net.safedata.performance.training.lab.nplus1;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lab_section")
public class SectionEntity {

    @Id
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private StoreEntity store;

    @OneToMany(mappedBy = "section", cascade = CascadeType.PERSIST)
    private List<ItemEntity> items = new ArrayList<>();

    protected SectionEntity() {
    }

    public SectionEntity(Long id, String name, StoreEntity store) {
        this.id = id;
        this.name = name;
        this.store = store;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StoreEntity getStore() {
        return store;
    }

    public List<ItemEntity> getItems() {
        return items;
    }
}
