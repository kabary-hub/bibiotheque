import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { UserRequest } from '../_model/users';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-update-user',
  templateUrl: './update-user.component.html',
  styleUrls: ['./update-user.component.css']
})
export class UpdateUserComponent implements OnInit {

  userId: number;
  user: UserRequest = { username: '', name: '', roleIds: [2] };
  roleLabel = 'User';
  envoiEnCours = false;
  erreur: string | null = null;

  constructor(
    private usersService: UsersService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userId = this.route.snapshot.params['userId'];
    this.usersService.getUserById(this.userId).subscribe({
      next: (data) => {
        this.user.username = data.username;
        this.user.name = data.name;
        const roles: string[] = (data as any).roles || [];
        const isAdmin = roles.indexOf('Admin') !== -1;
        this.user.roleIds = isAdmin ? [1] : [2];
        this.roleLabel = isAdmin ? 'Admin' : 'User';
      },
      error: (err: HttpErrorResponse) => {
        this.erreur = this.extraireMessage(err);
      }
    });
  }

  onRoleChange(valeur: string): void {
    this.user.roleIds = valeur === 'Admin' ? [1] : [2];
  }

  onSubmit(): void {
    this.erreur = null;
    this.envoiEnCours = true;
    this.usersService.updateUser(this.userId, this.user).subscribe({
      next: () => {
        this.envoiEnCours = false;
        this.router.navigate(['/users']);
      },
      error: (err: HttpErrorResponse) => {
        this.envoiEnCours = false;
        this.erreur = this.extraireMessage(err);
      }
    });
  }

  private extraireMessage(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'Le serveur est injoignable. Verifiez que le backend est demarre.';
    }
    const corps = err.error;
    if (corps && corps.message) {
      return corps.message;
    }
    return 'Erreur ' + err.status + ' - ' + err.statusText;
  }
}
