import { Controller, Get, Post, Put, Body, Param, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiResponse } from '@nestjs/swagger';
import { AppsService } from './apps.service';
import { BulkUpdateAppRestrictionsDto } from './dto/update-app-restriction.dto';
import { SyncInstalledAppsDto } from './dto/sync-installed-apps.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@ApiTags('Application & Gaming Restrictions')
@Controller('api/v1/apps')
export class AppsController {
  constructor(private readonly appsService: AppsService) {}

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('sync/:childId')
  @ApiOperation({ summary: 'Child device uploads installed applications inventory' })
  async syncInstalledApps(
    @Param('childId') childId: string,
    @Body() dto: SyncInstalledAppsDto,
  ) {
    return this.appsService.syncInstalledApps(childId, dto);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get(':childId')
  @ApiOperation({ summary: 'Parent retrieves all apps and restriction modes for child' })
  async getRestrictions(@Request() req, @Param('childId') childId: string) {
    return this.appsService.getAppRestrictions(req.user.parentId, childId);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Put(':childId/restrictions')
  @ApiOperation({ summary: 'Parent updates app restriction modes (Allow, Block, Time-Limit)' })
  async updateRestrictions(
    @Request() req,
    @Param('childId') childId: string,
    @Body() dto: BulkUpdateAppRestrictionsDto,
  ) {
    return this.appsService.updateRestrictions(req.user.parentId, childId, dto);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get(':childId/gaming')
  @ApiOperation({ summary: 'Parent gets gaming vault status and game limits' })
  async getGamingOverview(@Request() req, @Param('childId') childId: string) {
    return this.appsService.getGamingOverview(req.user.parentId, childId);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Put(':childId/gaming/schedule')
  @ApiOperation({ summary: 'Parent updates gaming limits, school restrictions, and bedtime rules' })
  async updateGamingSchedule(
    @Request() req,
    @Param('childId') childId: string,
    @Body()
    dto: {
      dailyGamingLimitMinutes?: number;
      blockGamesDuringSchool?: boolean;
      blockGamesDuringBedtime?: boolean;
    },
  ) {
    return this.appsService.updateGamingSchedule(req.user.parentId, childId, dto);
  }
}
