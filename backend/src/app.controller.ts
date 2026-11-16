import { Controller, Get } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';

@ApiTags('Health & Status')
@Controller()
export class AppController {
  @Get()
  @ApiOperation({ summary: 'System Health Check & Welcome' })
  getHealth() {
    return {
      status: 'ONLINE',
      service: 'FamilyGuard Parental Control Ecosystem Backend',
      version: '1.0.0',
      database: 'Supabase PostgreSQL Connected',
      documentation: '/api/docs',
      timestamp: new Date().toISOString(),
    };
  }
}
