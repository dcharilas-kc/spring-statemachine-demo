package demo.statemachine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "basket_order",
    uniqueConstraints = {
        @UniqueConstraint(name = "correlationIdUnique", columnNames = {"correlationId"})
    }
)
public class BasketOrder implements Serializable {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100)
  private String correlationId;
  
  @Column(length = 50)
  private String state;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @JoinColumn(name = "basket_order_id", referencedColumnName = "id", updatable = false, nullable = false)
  private List<StateTransition> transitions;
  
  @CreationTimestamp
  private OffsetDateTime dateCreated;
  
  @UpdateTimestamp
  private OffsetDateTime dateUpdated;
  
  private String terminationReason;

  private String customerId;
  
}
