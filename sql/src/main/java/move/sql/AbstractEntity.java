package move.sql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public abstract class AbstractEntity implements HasId {

  public static final String ID = "id";

  @JsonProperty(ID)
  @Column(name = ID, length = 32, nullable = false)
  protected String id;

  @JsonIgnore
  @NoColumn
  public boolean exists;

  public AbstractEntity() {
  }

  public AbstractEntity(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public AbstractEntity id(String id) {
    this.id = id;
    return this;
  }

  public boolean exists() {
    return exists;
  }
}
