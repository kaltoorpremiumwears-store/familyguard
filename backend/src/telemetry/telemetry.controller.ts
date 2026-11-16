import { Controller, Get, Post, Body, Param, Query, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiResponse } from '@nestjs/swagger';
import { TelemetryService } from './telemetry.service';
import { ReportUsageDto } from './dto/report-usage.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@ApiTags('Telemetry & Activity Reports')
@Controller('api/v1/telemetry')
export class TelemetryController {
  constructor(private readonly telemetryService: TelemetryService) {}

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('usage/:childId')
  @ApiOperation({ summary: 'Child device uploads foreground app usage statistics' })
  async reportUsage(
    @Param('childId') childId: string,
    @Body() dto: ReportUsageDto,
  ) {
    return this.telemetryService.recordUsage(childId, dto);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('reports/:childId/daily')
  @ApiOperation({ summary: 'Parent retrieves detailed daily usage report and category breakdown' })
  async getDailyReport(
    @Request() req,
    @Param('childId') childId: string,
    @Query('date') date?: string,
  ) {
    const targetDate = date || new Date().toISOString().split('T')[0];
    return this.telemetryService.getDailyReport(req.user.parentId, childId, targetDate);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('reports/:childId/weekly')
  @ApiOperation({ summary: 'Parent retrieves weekly screen time trends' })
  async getWeeklyTrends(
    @Request() req,
    @Param('childId') childId: string,
  ) {
    return this.telemetryService.getWeeklyTrends(req.user.parentId, childId);
  }
}
