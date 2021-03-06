import { AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { UtilService } from '../../../../../../services/util.service';
import { Voce } from '../../../../../../services/voce.service';
import { IFormComponent } from '../../../../../../classes/interfaces/IFormComponent';
import { GovpayService } from '../../../../../../services/govpay.service';
import { Parameters } from '../../../../../../classes/parameters';
import { Standard } from '../../../../../../classes/view/standard';
import { Dato } from '../../../../../../classes/view/dato';
import { AsyncFilterableSelectComponent } from '../../../../../async-filterable-select/async-filterable-select.component';

@Component({
  selector: 'link-tipi-pendenza-dominio-view',
  templateUrl: './tipi-pendenza-dominio-view.component.html',
  styleUrls: ['./tipi-pendenza-dominio-view.component.scss']
})
export class TipiPendenzaDominioViewComponent implements IFormComponent,  OnInit, AfterViewInit {
  @ViewChild('asyncTipiPendenza', { read: AsyncFilterableSelectComponent }) _asyncTipiPendenza: AsyncFilterableSelectComponent;

  @ViewChild('iSchemaBrowse') _iSchemaBrowse: ElementRef;
  @ViewChild('iLayoutBrowse') _iLayoutBrowse: ElementRef;

  @Input() fGroup: FormGroup;
  @Input() json: any;
  @Input() modified: boolean = false;
  @Input() parent: any;

  protected tipiPendenza_items: any[] = [];

  protected _voce = Voce;

  protected _generatori: any[] = UtilService.GENERATORI;
  protected _applicazioni: any[] = [];
  protected _doubleSet: any = {
    visualizzazione: false,
    schema: false,
    validazione: false,
    definizione: false,
    oggetto: false,
    messaggio: false,
    oggettoRicevuta: false,
    messaggioRicevuta: false,
    shadow_visualizzazione_ctrl: new FormControl(''),
    shadow_schema_ctrl: new FormControl(''),
    shadow_validazione_ctrl: new FormControl(''),
    shadow_definizione_ctrl: new FormControl(''),
    shadow_oggetto_ctrl: new FormControl(''),
    shadow_messaggio_ctrl: new FormControl(''),
    shadow_oggettoRicevuta_ctrl: new FormControl(''),
    shadow_messaggioRicevuta_ctrl: new FormControl('')
  };

  // Async filterable select
  protected _searching: boolean = false;
  protected _inputDisplay = (value: any) => {
    return value?value.descrizione:'';
  };
  protected _mapRisultati = () => {
    if(this.tipiPendenza_items && this.tipiPendenza_items.length > 1) {
      return this.tipiPendenza_items.length + ' risultati';
    }

    return '';
  };

  constructor(public gps: GovpayService, public us: UtilService) {
    this._elencoApplicazioni();
  }

  ngOnInit() {
    this.fGroup.addControl('tipoPendenza_ctrl', new FormControl('', [ Validators.required, this.requireMatch.bind(this) ]));
    this.fGroup.addControl('codificaIUV_ctrl', new FormControl(''));
    this.fGroup.addControl('abilita_ctrl', new FormControl(null));
    this.fGroup.addControl('pagaTerzi_ctrl', new FormControl(null));

    this.fGroup.addControl('visualizzazione_ctrl', new FormControl(''));
    this.fGroup.addControl('generatore_ctrl', new FormControl(''));
    this.fGroup.addControl('schema_ctrl', new FormControl(''));

    this.fGroup.addControl('validazione_ctrl', new FormControl(''));
    this.fGroup.addControl('tipoTrasformazione_ctrl', new FormControl(''));
    this.fGroup.addControl('definizione_ctrl', new FormControl(''));
    this.fGroup.addControl('inoltro_ctrl', new FormControl(''));

    this.fGroup.addControl('tipoTemplateAP_ctrl', new FormControl(''));
    this.fGroup.addControl('oggetto_ctrl', new FormControl(''));
    this.fGroup.addControl('messaggio_ctrl', new FormControl(''));
    this.fGroup.addControl('allegaPdf_ctrl', new FormControl(null));
    this.fGroup.addControl('abilitaAvviso_ctrl', new FormControl(null));

    this.fGroup.addControl('tipoTemplateAR_ctrl', new FormControl(''));
    this.fGroup.addControl('oggettoRicevuta_ctrl', new FormControl(''));
    this.fGroup.addControl('messaggioRicevuta_ctrl', new FormControl(''));
    this.fGroup.addControl('allegaPdfRicevuta_ctrl', new FormControl(null));
    this.fGroup.addControl('abilitaRicevuta_ctrl', new FormControl(null));
  }

  ngAfterViewInit() {
    setTimeout(() => {
      if(this.json) {
        this.fGroup.controls['tipoPendenza_ctrl'].disable();
        this.fGroup.controls['tipoPendenza_ctrl'].setValidators([ Validators.required ]);
        this.fGroup.controls['tipoPendenza_ctrl'].setValue(this.json);
        this._updateValues(this.json);
      } else {
        this._getTipiPendenza('');
      }
    });
  }

  protected requireMatch(control: FormControl): ValidationErrors | null {
    const selection: any = control.value;
    const _filtered = this.tipiPendenza_items.filter((item: any) => {
      return selection && (
        (selection.jsonP && selection.jsonP.descrizione === item.jsonP.descrizione) ||
        (selection.descrizione && selection.descrizione === item.jsonP.descrizione) ||
        (selection === item.jsonP.descrizione)
      );
    });
    if (_filtered.length === 0) {
      return { requireMatch: true };
    }

    return null;
  }

  protected _onOptionSelection(event: any) {
    if(event.original.option && event.original.option.value) {
      if(event.target) {
        this.fGroup.reset();
        this.fGroup.controls['tipoPendenza_ctrl'].setValue(event.original.option.value.jsonP);
        event.target.blur();
        this._updateValues(event.original.option.value.jsonP);
      }
    }
  }

  protected _asyncKeyUp(event: any) {
    this._asyncTipiPendenza.asyncOptions.clearAllTimeout();
    if(event.target.value) {
      const _delayFct = function () {
        this._asyncTipiPendenza.asyncOptions.clearAllTimeout();
        if(this._asyncTipiPendenza.isOpen()) {
          this._asyncTipiPendenza.close();
        }
        this._getTipiPendenza(event.target.value);
      }.bind(this);
      this._asyncTipiPendenza.asyncOptions.setTimeout(_delayFct, 800);
    } else {
      this._asyncTipiPendenza.close();
      this.tipiPendenza_items = [];
    }
  }

  /**
   * Check item index list by key
   * @param item: json item
   * @param {Parameters[]} checkList
   * @param {string} key
   * @returns {boolean}
   */
  // protected checkItemIndex(item: any, checkList: Parameters[], key: string): boolean {
  //   let _hasEntry: boolean = false;
  //   checkList.forEach((el) => {
  //     if(el.jsonP[key] == item[key]) {
  //       _hasEntry = true;
  //     }
  //   });
  //   return _hasEntry;
  // }

  protected _elencoApplicazioni() {
    this.gps.getDataService(UtilService.URL_APPLICAZIONI).subscribe(
      (response) => {
        this._applicazioni = response.body?(response.body.risultati || []):[];
        this.gps.updateSpinner(false);
      },
      (error) => {
        this._applicazioni = [];
        this.gps.updateSpinner(false);
        this.us.onError(error);
      }
    );
  }

  protected _lfsChange(event: any, controller: string) {
    if(event.type == 'file-selector-change') {
      if(event.value) {
        this.fGroup.controls[controller].setValidators([ Validators.required ]);
      } else {
        this.fGroup.controls[controller].clearValidators();
      }
      this.fGroup.controls[controller].updateValueAndValidity({ onlySelf: true });
    }
  }

  protected _lfsReset(event: any, controller: string) {
    if(event.type == 'file-selector-reset') {
      this.fGroup.controls[controller].setValue(this._doubleSet['shadow_'+controller].value);
    }
  }

  protected _getTipiPendenza(value: string) {
    let _service = UtilService.URL_TIPI_PENDENZA + '?descrizione=' + value + '&nonAssociati=' + this.parent.json.idDominio;
    this._searching = true;
    this.fGroup.controls['tipoPendenza_ctrl'].disable();
    this.gps.forkService([_service]).subscribe(
    (_responses) => {
      this._asyncTipiPendenza.asyncOptions.clearAllTimeout();
      this._searching = false;
      this.fGroup.controls['tipoPendenza_ctrl'].enable();
      let _body = _responses[0]['body'];
      let p: Parameters;
      this.tipiPendenza_items = _body['risultati'].map(function(item) {
        p = new Parameters();
        p.jsonP = item;
        p.model = this.mapNewItem(item);
        return p;
      }, this);
      if(!this._asyncTipiPendenza.isOpen()) {
        this._asyncTipiPendenza.open();
      }
      this.gps.updateSpinner(false);
    },
    (error) => {
      this._asyncTipiPendenza.asyncOptions.clearAllTimeout();
      this._searching = false;
      this.tipiPendenza_items = [];
      this.fGroup.controls['tipoPendenza_ctrl'].enable();
      this.gps.updateSpinner(false);
      this.us.onError(error);
    });
  }

  protected mapNewItem(item: any): Standard {
    let _std = new Standard();
    _std.titolo = new Dato({ label: item.descrizione, value: '' });
    _std.sottotitolo = new Dato({ label: Voce.ID_TIPO_PENDENZA+': ', value: item.idTipoPendenza });
    return _std;
  }

  protected _updateValues(json: any) {
    if(!this.fGroup.controls['tipoPendenza_ctrl'].disabled) {
      return;
    }

    if(json.valori && json.valori.visualizzazione) {
      this._doubleSet.visualizzazione = false;
    } else {
      this._doubleSet.visualizzazione = true;
    }
    if(json.valori && json.valori.form && json.valori.form.definizione) {
      this._doubleSet.schema = false;
    } else {
      this._doubleSet.schema = true;
    }
    if(json.valori && json.valori.validazione) {
      this._doubleSet.validazione = false;
    } else {
      this._doubleSet.validazione = true;
    }
    if(json.valori && json.valori.trasformazione && json.valori.trasformazione.definizione) {
      this._doubleSet.definizione = false;
    } else {
      this._doubleSet.definizione = true;
    }
    if(json.valori && json.valori.promemoriaAvviso && json.valori.promemoriaAvviso.oggetto) {
      this._doubleSet.oggetto = false;
    } else {
      this._doubleSet.oggetto = true;
    }
    if(json.valori && json.valori.promemoriaAvviso && json.valori.promemoriaAvviso.messaggio) {
      this._doubleSet.messaggio = false;
    } else {
      this._doubleSet.messaggio = true;
    }
    if(json.valori && json.valori.promemoriaRicevuta && json.valori.promemoriaRicevuta.oggetto) {
      this._doubleSet.oggettoRicevuta = false;
    } else {
      this._doubleSet.oggettoRicevuta = true;
    }
    if(json.valori && json.valori.promemoriaRicevuta && json.valori.promemoriaRicevuta.messaggio) {
      this._doubleSet.messaggioRicevuta = false;
    } else {
      this._doubleSet.messaggioRicevuta = true;
    }

    this._doubleSet[ 'shadow_visualizzazione_ctrl' ].setValue(json.visualizzazione?json.visualizzazione:'');
    this._doubleSet[ 'shadow_schema_ctrl' ].setValue(json.form?json.form.definizione:'');
    this._doubleSet[ 'shadow_validazione_ctrl' ].setValue(json.validazione || '');
    this._doubleSet[ 'shadow_definizione_ctrl' ].setValue(json.trasformazione?json.trasformazione.definizione:'');
    this._doubleSet[ 'shadow_oggetto_ctrl' ].setValue(json.promemoriaAvviso?json.promemoriaAvviso.oggetto:'');
    this._doubleSet[ 'shadow_messaggio_ctrl' ].setValue(json.promemoriaAvviso?json.promemoriaAvviso.messaggio:'');
    this._doubleSet[ 'shadow_oggettoRicevuta_ctrl' ].setValue(json.promemoriaRicevuta?json.promemoriaRicevuta.oggettoRicevuta:'');
    this._doubleSet[ 'shadow_messaggioRicevuta_ctrl' ].setValue(json.promemoriaRicevuta?json.promemoriaRicevuta.messaggioRicevuta:'');

    setTimeout(() => {
      if (json.valori && json.valori.codificaIUV) {
        this.fGroup.controls[ 'codificaIUV_ctrl' ].setValue(json.valori.codificaIUV);
      }
      if (json.valori) {
        this.fGroup.controls[ 'abilita_ctrl' ].setValue(json.valori.abilitato);
      }
      if (json.valori) {
        this.fGroup.controls[ 'pagaTerzi_ctrl' ].setValue(json.valori.pagaTerzi);
      }
      if (json.valori && json.valori.form && json.valori.form.tipo) {
        this.fGroup.controls[ 'generatore_ctrl' ].setValue(json.valori.form.tipo || '');
      }
      if (json.valori && json.valori.visualizzazione) {
        this.fGroup.controls[ 'visualizzazione_ctrl' ].setValue(json.valori.visualizzazione || '');
      } else {
        this.fGroup.controls[ 'visualizzazione_ctrl' ].setValue(json.visualizzazione?json.visualizzazione:'');
      }
      if (json.valori && json.valori.form && json.valori.form.definizione) {
        this.fGroup.controls[ 'schema_ctrl' ].setValue(json.valori.form.definizione || '');
      } else {
        this.fGroup.controls[ 'schema_ctrl' ].setValue(json.form?json.form.definizione:'');
      }
      if (json.valori && json.valori.validazione) {
        this.fGroup.controls[ 'validazione_ctrl' ].setValue(json.valori.validazione);
      } else {
        this.fGroup.controls[ 'validazione_ctrl' ].setValue(json.validazione || '');
      }
      if (json.valori && json.valori.trasformazione && json.valori.trasformazione.tipo) {
        this.fGroup.controls[ 'tipoTrasformazione_ctrl' ].setValue(json.valori.trasformazione.tipo);
      }
      if (json.valori && json.valori.trasformazione && json.valori.trasformazione.definizione) {
        this.fGroup.controls[ 'definizione_ctrl' ].setValue(json.valori.trasformazione.definizione);
      } else {
        this.fGroup.controls[ 'definizione_ctrl' ].setValue(json.trasformazione?json.trasformazione.definizione:'');
      }
      if (json.valori && json.valori.inoltro) {
        this.fGroup.controls[ 'inoltro_ctrl' ].setValue(json.valori.inoltro);
      }
      if (json.valori && json.valori.promemoriaAvviso && json.valori.promemoriaAvviso.tipo) {
        this.fGroup.controls[ 'tipoTemplateAP_ctrl' ].setValue(json.valori.promemoriaAvviso.tipo);
      }
      if (json.valori && json.valori.promemoriaAvviso && json.valori.promemoriaAvviso.oggetto) {
        this.fGroup.controls[ 'oggetto_ctrl' ].setValue(json.valori.promemoriaAvviso.oggetto);
      } else {
        this.fGroup.controls[ 'oggetto_ctrl' ].setValue(json.promemoriaAvviso?json.promemoriaAvviso.oggetto:'');
      }
      if (json.valori && json.valori.promemoriaAvviso && json.valori.promemoriaAvviso.messaggio) {
        this.fGroup.controls[ 'messaggio_ctrl' ].setValue(json.valori.promemoriaAvviso.messaggio);
      } else {
        this.fGroup.controls[ 'messaggio_ctrl' ].setValue(json.promemoriaAvviso?json.promemoriaAvviso.messaggio:'');
      }
      if (json.valori && json.valori.promemoriaAvviso) {
        this.fGroup.controls[ 'abilitaAvviso_ctrl' ].setValue(json.valori.promemoriaAvviso.abilitato);
        this.fGroup.controls[ 'allegaPdf_ctrl' ].setValue(json.valori.promemoriaAvviso.allegaPdf);
      }
      if (json.valori && json.valori.promemoriaRicevuta && json.valori.promemoriaRicevuta.tipo) {
        this.fGroup.controls[ 'tipoTemplateAR_ctrl' ].setValue(json.valori.promemoriaRicevuta.tipo);
      }
      if (json.valori && json.valori.promemoriaRicevuta && json.valori.promemoriaRicevuta.oggetto) {
        this.fGroup.controls[ 'oggettoRicevuta_ctrl' ].setValue(json.valori.promemoriaRicevuta.oggetto);
      } else {
        this.fGroup.controls[ 'oggettoRicevuta_ctrl' ].setValue(json.promemoriaRicevuta?json.promemoriaRicevuta.oggetto:'');
      }
      if (json.valori && json.valori.promemoriaRicevuta && json.valori.promemoriaRicevuta.messaggio) {
        this.fGroup.controls[ 'messaggioRicevuta_ctrl' ].setValue(json.valori.promemoriaRicevuta.messaggio);
      } else {
        this.fGroup.controls[ 'messaggioRicevuta_ctrl' ].setValue(json.promemoriaRicevuta?json.promemoriaRicevuta.messaggio:'');
      }
      if (json.valori && json.valori.promemoriaRicevuta) {
        this.fGroup.controls[ 'abilitaRicevuta_ctrl' ].setValue(json.valori.promemoriaRicevuta.abilitato);
        this.fGroup.controls[ 'allegaPdfRicevuta_ctrl' ].setValue(json.valori.promemoriaRicevuta.allegaPdf);
      }
    });

  }

  /**
   * Interface IFormComponent: Form controls to json object
   * @returns {any}
   */
  mapToJson(): any {
    let _info = this.fGroup.value;
    let _json:any = {};

    if(!this.fGroup.controls['tipoPendenza_ctrl'].disabled) {
      _json = _info['tipoPendenza_ctrl'];
    } else {
      _json = this.json;
    }
    _json.valori = {
      pagaTerzi: (_info['pagaTerzi_ctrl'] !== undefined)?_info['pagaTerzi_ctrl']:null,
      abilitato: (_info['abilita_ctrl'] !== undefined)?_info['abilita_ctrl']:null,
      codificaIUV: (_info['codificaIUV_ctrl'])?_info['codificaIUV_ctrl']:null,
      visualizzazione: _info['visualizzazione_ctrl'] || null,
      form: {
        tipo: _info['generatore_ctrl'] || null,
        definizione: _info['schema_ctrl'] || null
      },
      validazione: _info['validazione_ctrl'] || null,
      trasformazione: {
        tipo: _info['tipoTrasformazione_ctrl'] || null,
        definizione: _info['definizione_ctrl'] || null
      },
      inoltro: _info['inoltro_ctrl'] || null,
      promemoriaAvviso: {
        tipo: _info['tipoTemplateAP_ctrl'] || null,
        oggetto: _info['oggetto_ctrl'] || null,
        messaggio: _info['messaggio_ctrl'] || null,
        abilitato: (_info['abilitaAvviso_ctrl'] !== undefined)?_info['abilitaAvviso_ctrl']:null,
        allegaPdf: (_info['allegaPdf_ctrl'] !== undefined)?_info['allegaPdf_ctrl']:null
      },
      promemoriaRicevuta: {
        tipo: _info['tipoTemplateAR_ctrl'] || null,
        oggetto: _info['oggettoRicevuta_ctrl'] || null,
        messaggio: _info['messaggioRicevuta_ctrl'] || null,
        abilitato: (_info['abilitaRicevuta_ctrl'] !== undefined)?_info['abilitaRicevuta_ctrl']:null,
        allegaPdf: (_info['allegaPdfRicevuta_ctrl'] !== undefined)?_info['allegaPdfRicevuta_ctrl']:null
      }
    };

    if(!_json.valori.promemoriaAvviso.oggetto) {
      _json.valori.promemoriaAvviso.messaggio = null;
    }
    if(!_json.valori.promemoriaRicevuta.oggetto) {
      _json.valori.promemoriaRicevuta.messaggio = null;
    }

    return _json;
  }

}
