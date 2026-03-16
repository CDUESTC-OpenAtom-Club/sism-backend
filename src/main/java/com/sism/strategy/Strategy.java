package com.sism.strategy;

/**
 * Minimal Strategy domain placeholder for future DDD alignment.
 */
public class Strategy {
  private Long id;
  private String name;

  public Strategy() {}

  public Strategy(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}
