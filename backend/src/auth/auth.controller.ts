import { Controller, Post, Get, Delete, Body, HttpCode, HttpStatus, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { JwtAuthGuard } from './jwt-auth.guard';

@ApiTags('Parent Authentication')
@Controller('api/v1/auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register')
  @ApiOperation({ summary: 'Register parent account with Verifiable Parental Consent' })
  @ApiResponse({ status: 201, description: 'Parent registered successfully' })
  @ApiResponse({ status: 400, description: 'Parental consent missing or invalid input' })
  @ApiResponse({ status: 409, description: 'Email already registered' })
  async register(@Body() dto: RegisterDto) {
    return this.authService.register(dto);
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Authenticate parent and retrieve JWT token' })
  @ApiResponse({ status: 200, description: 'Authentication successful' })
  @ApiResponse({ status: 401, description: 'Invalid credentials' })
  async login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @Get('data-export')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'GDPR / COPPA Article 20 Right to Data Portability export' })
  @ApiResponse({ status: 200, description: 'Complete family data export returned in JSON format' })
  async exportData(@Request() req: any) {
    return this.authService.exportData(req.user.id);
  }

  @Delete('account')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'GDPR Article 17 Right to Erasure / Right to be Forgotten account deletion' })
  @ApiResponse({ status: 200, description: 'Account and all associated family data permanently deleted' })
  async deleteAccount(@Request() req: any) {
    return this.authService.deleteAccount(req.user.id);
  }
}
