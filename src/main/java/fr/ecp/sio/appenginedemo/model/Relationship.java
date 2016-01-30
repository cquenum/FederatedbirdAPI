package fr.ecp.sio.appenginedemo.model;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cquenum on 30/01/2016.
 */

@Entity
public class Relationship {

    @Id
    public Long id;

    @Index
    @Parent
    public Ref<User> user;

    @Load
    @Index
    public List<Ref<User>> followed = new ArrayList<>();

    @Load
    @Index
    public List<Ref<User>> followers = new ArrayList<>();
}
