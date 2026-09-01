import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { UserRequest } from '../_model/users';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css']
})
export class RegistrationComponent {

  user: UserRequest = { username: '', name: '', password: '', roleIds: [2] };
  roleLabel = 'User';
  envoiEnCours = false;
  erreur: string | null = null;

  constructor(private usersService: UsersService, private router: Router) {}

  onRoleChange(valeur: string): void {
    this.user.roleIds = valeur === 'Admin' ? [1] : [2];
  }

  onSubmit(): void {
    this.erreur = null;
    this.envoiEnCours = true;
    this.usersService.createUser(this.user).subscribe({
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
