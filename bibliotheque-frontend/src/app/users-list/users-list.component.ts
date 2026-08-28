import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Users } from '../_model/users';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css']
})
export class UsersListComponent implements OnInit {

  users: Users[];

  constructor(private usersService: UsersService, private router: Router) {}

  ngOnInit(): void { this.getUsers(); }

  private getUsers() {
    this.usersService.getUsersList().subscribe(data => { this.users = data; });
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

  userDetails(userId: number) { this.router.navigate(['user-details', userId]); }
  updateUser(userId: number)  { this.router.navigate(['update-user', userId]); }

}
