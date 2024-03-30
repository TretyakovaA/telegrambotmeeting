package org.example.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "link")
    private String link;

    @Column(name = "creator")
    private long creator;

}
