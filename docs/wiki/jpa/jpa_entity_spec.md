## JPA 엔티티 설계에 대한 기술 명세입니다.

### Column 작성 시 참고사항

- 컬럼은 반드시 `@Column` 어노테이션을 사용하여 명시적으로 매핑해야 합니다.
- `@Column` 어노테이션의 `name` 속성은 반드시 명시해야 합니다.
- `@Column` 어노테이션의 `nullable` 속성은 null이 허용 안될 경우에만 false로 명시해야 합니다.
- `@Comment` 어노테이션을 사용하여 컬럼에 대한 설명을 명시해야 합니다.

```java

@Comment("내용")
@Column(name = "content", nullable = false)
private String content;

```

----

### Enum 타입 매핑 시 참고사항

- Enum 타입은 반드시 `@Enumerated` 어노테이션을 사용하여 명시적으로 매핑해야 합니다.
- `@Enumerated` 어노테이션의 `EnumType` 속성은 반드시 `EnumType.STRING`으로 명시해야 합니다.
    - EnumType.ORDINAL은 사용하지 않습니다.

```java

@Comment("용량타입")
@Column(name = "size_type", nullable = false)
@Enumerated(EnumType.STRING)
private SizeType sizeType;

```

----

### 연관관계 매핑 Column 작성 시 참고사항

- `@ManyToOne` 어노테이션을 사용하여 다대일 관계를 매핑할 때 반드시 `fetch` 속성을 `FetchType.LAZY`로 명시해야 합니다.
    - 요구사항으로 인해 `FetchType.EAGER`로 변경할 경우에는 매우 신중하게 변경하고 협의하길 바랍니다.
- `@JoinColumn` 어노테이션을 사용하여 외래키를 명시해야 합니다.
    - `name` 속성은 외래키 컬럼명을 명시해야 합니다.
    - `referencedColumnName` 속성은 참조할 대상 엔티티의 기본키 컬럼명을 명시해야 합니다.
    - `nullable` 속성은 null이 허용 안될 경우에만 false로 명시해야 합니다.
        - ex) `Review`의 경우 작성자가 반드시 필요하기 때문에 nullable = false로 명시해야 합니다.

```java

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
private Users user;
// name = "user_id" : 외래키 컬럼명 해당 엔티티가 참조하는 엔티티의 기본키 컬럼명
//referencedColumnName = "id" : 참조할 대상 엔티티의 기본키 컬럼명 Users 엔티티의 기본키 컬럼명
```

------

### 양방향 연관관계 매핑 시 참고사항

> 양방향 연관관계란 두 엔티티가 서로 참조하는 관계를 의미합니다.<br>
> 양방향 연관관계를 매핑할 때는 주인 엔티티와 연관관계의 주인이 아닌 엔티티를 구분해야 합니다.<br>
> 주인 엔티티는 외래키를 관리하고 연관관계의 주인이 아닌 엔티티는 외래키를 관리하지 않습니다.<br>
> 따라서 주인 엔티티에서는 `@JoinColumn` 어노테이션을 사용하여 외래키를 명시해야 합니다.<br>
> 연관관계의 주인이 아닌 엔티티에서는 `mappedBy` 속성을 사용하여 주인 엔티티의 필드명을 명시해야 합니다.<br>
> 양방향관계 예시 ) User(1) - Review(N) , Board(1) - Comment(N) , Product(1) - Order(N)

- 양방향 연관관계 매핑 시에는 반드시 `mappedBy` 속성을 사용하여 주인 엔티티를 명시해야 합니다.
    - 주인 엔티티가 아닌 엔티티에서는 `mappedBy` 속성을 사용하여 주인 엔티티의 필드명을 명시해야 합니다.
    - 주인 엔티티에서는 `mappedBy` 속성을 사용하지 않습니다.
- `@OneToMany` 어노테이션을 사용하여 양방향 관계를 매핑할 때 반드시 `fetch` 속성을 `FetchType.LAZY`로 명시해야 합니다.

```java

@Entity
class Review {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private Users user;
}

@Entity
class Users {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
}
```



