import { Controller, Get, Post, Delete, Body, Param, Query, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiResponse } from '@nestjs/swagger';
import { LocationService } from './location.service';
import { ReportLocationDto } from './dto/report-location.dto';
import { CreateGeofenceDto } from './dto/geofence-zone.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@ApiTags('Location Radar & Geofencing')
@Controller('api/v1/location')
export class LocationController {
  constructor(private readonly locationService: LocationService) {}

  @Post('report/:childId')
  @ApiOperation({ summary: 'Child device reports live GPS coordinates & battery' })
  async reportLocation(
    @Param('childId') childId: string,
    @Body() dto: ReportLocationDto,
  ) {
    return this.locationService.reportLocation(childId, dto);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('latest/:childId')
  @ApiOperation({ summary: 'Parent retrieves current live location pin and active geofences' })
  async getLatestLocation(@Request() req, @Param('childId') childId: string) {
    return this.locationService.getLatestLocation(req.user.parentId, childId);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('history/:childId')
  @ApiOperation({ summary: 'Parent retrieves location history breadcrumbs trail' })
  async getLocationHistory(
    @Request() req,
    @Param('childId') childId: string,
    @Query('limit') limit?: number,
  ) {
    return this.locationService.getLocationHistory(req.user.parentId, childId, limit ? Number(limit) : 50);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('geofences/:childId')
  @ApiOperation({ summary: 'Parent creates a geofence safe zone (Home, School, etc.)' })
  async createGeofence(
    @Request() req,
    @Param('childId') childId: string,
    @Body() dto: CreateGeofenceDto,
  ) {
    return this.locationService.createGeofence(req.user.parentId, childId, dto);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('geofences/:childId')
  @ApiOperation({ summary: 'Parent lists all geofences for child' })
  async getGeofences(@Request() req, @Param('childId') childId: string) {
    return this.locationService.getGeofences(req.user.parentId, childId);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Delete('geofences/:childId/:geofenceId')
  @ApiOperation({ summary: 'Parent deletes a geofence zone' })
  async deleteGeofence(
    @Request() req,
    @Param('childId') childId: string,
    @Param('geofenceId') geofenceId: string,
  ) {
    return this.locationService.deleteGeofence(req.user.parentId, childId, geofenceId);
  }
}
