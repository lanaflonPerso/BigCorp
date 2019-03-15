package com.training.spring.bigcorp.repository;

import com.training.spring.bigcorp.model.Captor;
import com.training.spring.bigcorp.model.Measure;
import com.training.spring.bigcorp.model.RealCaptor;
import com.training.spring.bigcorp.model.Site;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@DataJpaTest
@ComponentScan
public class MeasureDaoImplTest {

    @Autowired
    private MeasureDao measureDao;

    @Autowired
    private EntityManager em;

    @Test
    public void findById() {
        Optional<Measure> measure = measureDao.findById(-1L);

        Assertions.assertThat(measure.get().getInstant()).isEqualTo(Instant.parse("2018-08-09T11:00:00.000Z"));
        Assertions.assertThat(measure.get().getValueInWatt()).isEqualTo(1_000_000);
        Assertions.assertThat(measure.get().getCaptor().getName()).isEqualTo("Eolienne");
        Assertions.assertThat(measure.get().getCaptor().getSite().getName()).isEqualTo("Bigcorp Lyon");
    }
    @Test
    public void findByIdShouldReturnNullWhenIdUnknown() {
        Optional<Measure> measure = measureDao.findById(-1000L);
        Assertions.assertThat(measure).isEmpty();
    }
    @Test
    public void findAll() {
        List<Measure> measures = measureDao.findAll();
        Assertions.assertThat(measures).hasSize(10);
    }
    @Test
    public void create() {
        Captor captor = new RealCaptor("Eolienne", new Site("site"));
        captor.setId("c1");
        Assertions.assertThat(measureDao.findAll()).hasSize(10);
        measureDao.save(new Measure(Instant.now(), 2_333_666, captor));
        Assertions.assertThat(measureDao.findAll()).hasSize(11);
    }
    @Test
    public void update() {
        Optional<Measure> measure = measureDao.findById(-1L);
        Assertions.assertThat(measure.get().getValueInWatt()).isEqualTo(1_000_000);
        measure.get().setValueInWatt(2_333_666);
        measureDao.save(measure.get());
        measure = measureDao.findById(-1L);
        Assertions.assertThat(measure.get().getValueInWatt()).isEqualTo(2_333_666);
    }
    @Test
    public void deleteById() {
        Assertions.assertThat(measureDao.findAll()).hasSize(10);
        measureDao.delete(measureDao.findById(-1L).get());
        Assertions.assertThat(measureDao.findAll()).hasSize(9);
    }

    @Test
    public void deleteByCaptorId() {
        Assertions.assertThat(measureDao.findAll().stream().filter(m ->
                m.getCaptor().getId().equals("c1"))).hasSize(5);
        measureDao.deleteByCaptorId("c1");
        Assertions.assertThat(measureDao.findAll().stream().filter(m ->
                m.getCaptor().getId().equals("c1"))).isEmpty();
    }

    @Test
    public void preventConcurrentWrite() {
        Measure measure = measureDao.getOne(-1L);
        // A la base le numéro de version est à sa valeur initiale
        Assertions.assertThat(measure.getVersion()).isEqualTo(0);
        // On detache cet objet du contexte de persistence
        em.detach(measure);
        measure.setValueInWatt(1);
        // On force la mise à jour en base (via le flush) et on vérifie que l'objet retourné
        // et attaché à la session a été mis à jour
        Measure attachedMeasure = measureDao.save(measure);
        em.flush();
        Assertions.assertThat(attachedMeasure.getValueInWatt()).isEqualTo(1);
        Assertions.assertThat(attachedMeasure.getVersion()).isEqualTo(1);
        // Si maintenant je réessaie d'enregistrer captor, comme le numéro de version est
        // à 0 je dois avoir une exception
        Assertions.assertThatThrownBy(() -> measureDao.save(measure))
                .isExactlyInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}