import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Users } from '../_model/users';
import { UsersService } from '../_service/users.service';
import { ActionMenuItem } from '../_shared/action-menu/action-menu.component';

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css']
})
export class UsersListComponent implements OnInit {

  users: Users[] = [];
  userToDelete: Users | null = null;

  // Pagination
  page = 1;
  pageSize = 10;

  menuItems: ActionMenuItem[] = [];

  constructor(private usersService: UsersService, private router: Router) {}

  ngOnInit(): void { this.getUsers(); }

  private getUsers() {
    this.usersService.getUsersList().subscribe(data => { this.users = data; });
  }

  get pagedUsers(): Users[] {
    const start = (this.page - 1) * this.pageSize;
    return this.users.slice(start, start + this.pageSize);
  }

  onPageChange(p: number) { this.page = p; }

  getMenuItems(user: Users): ActionMenuItem[] {
    const items: ActionMenuItem[] = [];
    if (this.isUser(user)) {
      items.push({ label: 'Historique' });
    }
    items.push({ label: 'Modifier' });
    items.push({ label: 'Supprimer', danger: true });
    return items;
  }

  onMenuAction(user: Users, actionIndex: number) {
    const items = this.getMenuItems(user);
    const action = items[actionIndex];
    if (action.label === 'Historique') this.router.navigate(['user-details', user.userId]);
    else if (action.label === 'Modifier') this.router.navigate(['update-user', user.userId]);
    else if (action.label === 'Supprimer') this.confirmDelete(user);
  }

  getRoleLabel(user: Users): string {
    const roles: string[] = (user as any).roles || [];
    return roles[0] || (user.role?.[0]?.roleName) || '—';
  }

  getRoleBadge(user: Users): string {
    return this.getRoleLabel(user) === 'Admin' ? 'badge--primary' : 'badge--neutral';
  }

  isUser(user: Users): boolean {
    return this.getRoleLabel(user) === 'User';
  }

  confirmDelete(user: Users) { this.userToDelete = user; }

  deleteUser(userId: number) {
    this.userToDelete = null;
    this.usersService.deleteUser(userId).subscribe(() => this.getUsers());
  }

  userDetails(userId: number) { this.router.navigate(['user-details', userId]); }
  updateUser(userId: number)  { this.router.navigate(['update-user', userId]); }
}
