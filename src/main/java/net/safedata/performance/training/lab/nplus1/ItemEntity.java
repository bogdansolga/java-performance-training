package net.safedata.performance.training.lab.nplus1;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lab_item")
public class ItemEntity {

    @Id
    private Long id;

    private String name;

    private double price;

    @ManyToOne(fetch = FetchType.LAZY)
    private SectionEntity section;

    protected ItemEntity() {
    }

    public ItemEntity(Long id, String name, double price, SectionEntity section) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.section = section;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public SectionEntity getSection() {
        return section;
    }
}
