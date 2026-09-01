import { Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';

export interface ActionMenuItem {
  label: string;
  icon?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-action-menu',
  templateUrl: './action-menu.component.html',
  styleUrls: ['./action-menu.component.css']
})
export class ActionMenuComponent {

  @Input() items: ActionMenuItem[] = [];
  @Output() action = new EventEmitter<number>();

  open = false;
  activeIndex = -1;

  constructor(private eRef: ElementRef) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.open = false;
      this.activeIndex = -1;
    }
  }

  toggle(event: Event) {
    event.stopPropagation();
    this.open = !this.open;
  }

  onAction(index: number) {
    this.open = false;
    this.activeIndex = index;
    this.action.emit(index);
  }
}
