--GP-557
CREATE TABLE avvisi
(
       cod_dominio VARCHAR(35) NOT NULL,
       iuv VARCHAR(35) NOT NULL,
       -- Precisione ai millisecondi supportata dalla versione 5.6.4, se si utilizza una versione precedente non usare il suffisso '(3)'
       data_creazione TIMESTAMP(3) NOT NULL DEFAULT 0,
       stato VARCHAR(255) NOT NULL,
       pdf MEDIUMBLOB,
       -- fk/pk columns
       id BIGINT AUTO_INCREMENT,
       -- check constraints
       CONSTRAINT chk_avvisi_1 CHECK (stato IN ('DA_STAMPARE','STAMPATO')),
       -- fk/pk keys constraints
       CONSTRAINT pk_avvisi PRIMARY KEY (id)
)ENGINE INNODB CHARACTER SET latin1 COLLATE latin1_general_cs;

-- index
CREATE INDEX index_avvisi_1 ON avvisi (cod_dominio,iuv);

insert into sonde(nome, classe, soglia_warn, soglia_error) values ('generazione-avvisi', 'org.openspcoop2.utils.sonde.impl.SondaBatch', 3600000, 21600000);

ALTER TABLE domini ADD COLUMN cbill VARCHAR(255);
ALTER TABLE uo ADD COLUMN uo_area VARCHAR(255);
ALTER TABLE uo ADD COLUMN uo_url_sito_web VARCHAR(255);
ALTER TABLE uo ADD COLUMN uo_email VARCHAR(255);
ALTER TABLE uo ADD COLUMN uo_pec VARCHAR(255);



alter table tracciati DROP CHECK chk_tracciati_1;
alter table tracciati add CONSTRAINT chk_tracciati_1 CHECK (stato IN ('ANNULLATO','NUOVO','IN_CARICAMENTO','CARICAMENTO_OK','CARICAMENTO_KO','STAMPATO'));

alter table tracciati add COLUMN tipo_tracciato VARCHAR(255);
alter table tracciati add CONSTRAINT chk_tracciati_2 CHECK (tipo_tracciato IN ('VERSAMENTI','INCASSI'));
update tracciati set tipo_tracciato = 'VERSAMENTI';
alter table tracciati MODIFY COLUMN tipo_tracciato NOT NULL;

alter table operazioni add COLUMN cod_dominio VARCHAR(35);
alter table operazioni add COLUMN iuv VARCHAR(35);
alter table operazioni add COLUMN trn VARCHAR(35);


alter table operazioni DROP CHECK chk_operazioni_1;
alter table operazioni add CONSTRAINT chk_operazioni_1 CHECK (tipo_operazione IN ('ADD','DEL','INC','N_V'));
alter table operazioni DROP CHECK chk_operazioni_2;
alter table operazioni add CONSTRAINT chk_operazioni_2 CHECK (stato IN ('NON_VALIDO','ESEGUITO_OK','ESEGUITO_KO'));

